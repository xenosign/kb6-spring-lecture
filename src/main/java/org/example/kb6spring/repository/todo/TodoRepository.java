package org.example.kb6spring.repository.todo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.todo.Todo;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Repository
public class TodoRepository {
    private final EntityManager em;

    public List<Todo> findAll() {
        String jpql = "SELECT t FROM Todo t";
        return em.createQuery(jpql, Todo.class).getResultList();
    }

    public void save(Todo todo) {
        em.persist(todo);
    }

    public Todo findById(Integer id) {
        return em.find(Todo.class, id);
    }

    public Optional<Todo> findById2(Integer id) {
        return Optional.ofNullable(em.find(Todo.class, id));
    }

    public void delete(Integer id) {
        Todo todo = em.find(Todo.class, id);
        if (todo != null) em.remove(todo);
    }

    public void updateDone(Todo todo) {
        todo.setDone(!todo.getDone());

        // 자동 감지로 반영이 됩니다! :)
        // em.merge(todo);
    }
}
