package ru.netology.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_files_user_filename",
                        columnNames = {"user_username", "filename"}
                )
        }
)
public class StorageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private byte[] fileContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_username", nullable = false)
    private User user;
}