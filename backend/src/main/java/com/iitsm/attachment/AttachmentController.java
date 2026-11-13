package com.iitsm.attachment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AttachmentController {
  private final AttachmentService service;

  public AttachmentController(AttachmentService s) {
    service = s;
  }

  @GetMapping("/incidents/{id}/attachments")
  public Object list(@PathVariable Long id) {
    return service.list(id);
  }

  @PostMapping(value = "/incidents/{id}/attachments", consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public Object upload(@PathVariable Long id, @RequestPart("file") MultipartFile file)
      throws IOException {
    return service.upload(id, file);
  }

  @GetMapping("/attachments/{id}")
  public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
    var a = service.metadata(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("X-Content-Type-Options", "nosniff")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(a.filename, StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(service.download(id));
  }

  @DeleteMapping("/attachments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
