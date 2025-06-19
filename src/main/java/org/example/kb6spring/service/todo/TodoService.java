package org.example.kb6spring.service.todo;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.domain.todo.Todo;
import org.example.kb6spring.repository.todo.TodoRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;

    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    public void save(Todo todo) {
        todoRepository.save(todo);
    }

    public void delete(Integer id) {
        todoRepository.delete(id);
    }

    public Todo findById(Integer id) {
        return todoRepository.findById(id);
    }

    public void updateDone(Integer id) {
        Todo todo = findById(id);

        if (todo == null) {
            throw new EntityNotFoundException("해당 ID를 가진 todo 가 없습니다. id : " + id);
        }

        todoRepository.updateDone(todo);
    }

    public void updateDone2(Integer id) {
        Todo todo = todoRepository.findById2(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID 를 가진 todo 가 없습니다. id : " + id));

        todoRepository.updateDone(todo);
    }
}
