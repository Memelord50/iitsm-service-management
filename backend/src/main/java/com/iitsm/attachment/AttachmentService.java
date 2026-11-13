package com.iitsm.attachment;

import com.iitsm.audit.Events;
import com.iitsm.auth.Access;
import com.iitsm.common.*;
import com.iitsm.incident.IncidentService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AttachmentService {
  private final Store db;
  private final Access access;
  private final IncidentService incidents;
  private final FileStorage storage;
  private final Events events;
  private final Clock clock;

  public AttachmentService(Store d, Access a, IncidentService i, FileStorage s, Events e, Clock c) {
    db = d;
    access = a;
    incidents = i;
    storage = s;
    events = e;
    clock = c;
  }

  public Object list(Long id) {
    incidents.read(id);
    return db.list(Attachment.class, "where e.incidentId=?1 and e.deleted=false", id).stream()
        .map(a -> Views.of(a, "id,filename,contentType,sizeBytes,uploaderId,createdAt"))
        .toList();
  }

  public Object upload(Long id, MultipartFile file) throws IOException {
    incidents.read(id);
    Problem.require(
        !file.isEmpty() && file.getSize() <= 5 * 1024 * 1024,
        "File must be between 1 byte and 5 MB");
    byte[] bytes = file.getBytes();
    String name =
        Optional.ofNullable(file.getOriginalFilename())
            .orElse("attachment")
            .replaceAll("[^a-zA-Z0-9._ -]", "_");
    Problem.require(name.length() <= 200, "Filename too long");
    String type = detect(name, bytes);
    String key = storage.put(bytes);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED)
              try {
                storage.delete(key);
              } catch (IOException ignored) {
              }
          }
        });
    var a = new Attachment();
    a.incidentId = id;
    a.uploaderId = access.me().id;
    a.filename = name;
    a.contentType = type;
    a.sizeBytes = bytes.length;
    a.storageKey = key;
    a.createdAt = clock.instant();
    db.save(a);
    events.record(access.me().id, "ATTACHMENT_UPLOADED", "INCIDENT", id, name);
    return Views.of(a, "id,filename,contentType,sizeBytes,createdAt");
  }

  static String detect(String name, byte[] b) {
    String lower = name.toLowerCase();
    if (lower.endsWith(".pdf")
        && b.length > 4
        && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) return "application/pdf";
    if (lower.endsWith(".png")
        && b.length >= 8
        && Arrays.equals(Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      return "image/png";
    if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        && b.length >= 3
        && (b[0] & 255) == 255
        && (b[1] & 255) == 216
        && (b[2] & 255) == 255) return "image/jpeg";
    if (lower.endsWith(".txt")
        && new String(b, StandardCharsets.UTF_8).chars().noneMatch(c -> c == 0))
      return "text/plain";
    throw new Problem(422, "Allowed file types: validated PDF, PNG, JPEG and plain text");
  }

  public Attachment metadata(Long id) {
    var a = db.get(Attachment.class, id);
    incidents.read(a.incidentId);
    if (a.deleted) throw new Problem(404, "Attachment deleted");
    return a;
  }

  public byte[] download(Long id) throws IOException {
    return storage.get(metadata(id).storageKey);
  }

  public void delete(Long id) {
    var a = metadata(id);
    Problem.require(
        access.me().role.equals("ADMIN") || a.uploaderId.equals(access.me().id),
        "Only uploader or administrator may delete");
    a.deleted = true;
    events.record(
        access.me().id,
        "ATTACHMENT_DELETED",
        "INCIDENT",
        a.incidentId,
        a.filename); /* Retain bytes for audit/retention; deleted metadata denies subsequent downloads. */
  }
}
