package org.example.kb6spring.domain.todo;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="todo")
public class Todo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "todo", nullable = false)
    private String todo;
    @Column(name = "done", nullable = true)
    private String done;
}
