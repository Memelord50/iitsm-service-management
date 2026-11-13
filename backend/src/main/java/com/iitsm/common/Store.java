package com.iitsm.common;

import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class Store {
  @PersistenceContext private EntityManager em;

  public <T> T save(T entity) {
    em.persist(entity);
    return entity;
  }

  public <T> T get(Class<T> type, Long id) {
    T v = em.find(type, id);
    if (v == null) throw new Problem(404, "Record not found");
    return v;
  }

  public <T> T lock(Class<T> type, Long id) {
    T v = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    if (v == null) throw new Problem(404, "Record not found");
    return v;
  }

  public <T> List<T> list(Class<T> type, String clause, Object... args) {
    var q = em.createQuery("select e from " + type.getSimpleName() + " e " + clause, type);
    for (int i = 0; i < args.length; i++) q.setParameter(i + 1, args[i]);
    return q.getResultList();
  }

  public <T> List<T> page(Class<T> type, String clause, int page, int size, Object... args) {
    var q = em.createQuery("select e from " + type.getSimpleName() + " e " + clause, type);
    for (int i = 0; i < args.length; i++) q.setParameter(i + 1, args[i]);
    return q.setFirstResult(page * size).setMaxResults(size).getResultList();
  }

  public void flush() {
    em.flush();
  }
}
