package com.iitsm.search;

import com.iitsm.auth.Access;
import com.iitsm.common.*;
import com.iitsm.incident.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchService {
  @PersistenceContext private EntityManager em;
  private final Access access;
  private final IncidentService incidents;

  public SearchService(Access a, IncidentService i) {
    access = a;
    incidents = i;
  }

  public Object search(Map<String, String> f) {
    var u = access.me();
    List<String> where = new ArrayList<>();
    Map<String, Object> params = new HashMap<>();
    if (!u.role.equals("ADMIN")) {
      where.add(u.role.equals("EMPLOYEE") ? "i.reporterId=:viewer" : "i.technicianId=:viewer");
      params.put("viewer", u.id);
    }
    for (String field :
        List.of("status", "priority", "serviceId", "categoryId", "reporterId", "technicianId")) {
      String value = f.get(field);
      if (value != null && !value.isBlank()) {
        where.add("i." + field + "=:" + field);
        params.put(field, field.endsWith("Id") ? Long.valueOf(value) : value);
      }
    }
    if (f.containsKey("q") && !f.get("q").isBlank()) {
      where.add(
          "(lower(i.title) like :q or lower(i.reference) like :q or lower(i.description) like :q)");
      params.put("q", "%" + f.get("q").toLowerCase() + "%");
    }
    Instant from = date(f, "from");
    Instant to = date(f, "to");
    Instant before = date(f, "before");
    if ((from != null && to != null && from.isAfter(to))
        || (from != null && before != null && !from.isBefore(before))) {
      throw new Problem(400, "Start date must not be after the end date");
    }
    if (from != null) {
      where.add("i.createdAt>=:from");
      params.put("from", from);
    }
    if (to != null) {
      where.add("i.createdAt<=:to");
      params.put("to", to);
    }
    if (before != null) {
      where.add("i.createdAt<:before");
      params.put("before", before);
    }
    String sla = f.get("slaStatus");
    if ("BREACHED".equals(sla))
      where.add(
          "exists (select c.id from SlaCycle c,SlaSignal s where c.incidentId=i.id and"
              + " s.cycleId=c.id and s.threshold=100)");
    else if ("WARNING".equals(sla))
      where.add(
          "exists (select c.id from SlaCycle c,SlaSignal s where c.incidentId=i.id and"
              + " s.cycleId=c.id and s.threshold<100)");
    else if ("ACTIVE".equals(sla))
      where.add(
          "exists (select c.id from SlaCycle c where c.incidentId=i.id and c.stoppedAt is null)");
    String clause = where.isEmpty() ? "" : " where " + String.join(" and ", where);
    int page = Math.max(0, Integer.parseInt(f.getOrDefault("page", "0"))),
        size = Math.max(1, Math.min(100, Integer.parseInt(f.getOrDefault("size", "20"))));
    String sort = f.getOrDefault("sort", "createdAt"),
        direction = f.getOrDefault("direction", "desc");
    if (!Set.of("createdAt", "reference", "status", "priority", "title").contains(sort))
      sort = "createdAt";
    if (!direction.equals("asc")) direction = "desc";
    String order = sort.equals("priority")
        ? "case i.priority when 'LOW' then 1 when 'MEDIUM' then 2 when 'HIGH' then 3 else 4 end"
        : "i." + sort;
    var count = em.createQuery("select count(i) from Incident i" + clause, Long.class);
    var rows =
        em.createQuery(
            "select i from Incident i"
                + clause
                + " order by "
                + order
                + " "
                + direction
                + ",i.id desc",
            Incident.class);
    params.forEach(
        (k, v) -> {
          count.setParameter(k, v);
          rows.setParameter(k, v);
        });
    return Map.of(
        "items",
        rows.setFirstResult(page * size).setMaxResults(size).getResultList().stream()
            .map(incidents::summary)
            .toList(),
        "total",
        count.getSingleResult(),
        "page",
        page,
        "size",
        size);
  }
  private static Instant date(Map<String, String> filters, String field) {
    String value = filters.get(field);
    if (value == null || value.isBlank()) return null;
    try {
      return Instant.parse(value);
    } catch (java.time.format.DateTimeParseException e) {
      throw new Problem(400, "Invalid " + field + " timestamp. Use an ISO-8601 timestamp.");
    }
  }
}
