package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movies")
public class Movie {
    @Id
    private String id;
    
    private String title;
    private String originalTitle;
    private String poster;
    private String trailer;
    private Integer duration;
    private LocalDate releaseDate;
    private List<String> directors;
    private List<String> genres;
    private String ageRating;
    private String description;
    private String subtitles;
    private String status;
    private List<String> tags;
    private Boolean isActive;
    private String country;
}
