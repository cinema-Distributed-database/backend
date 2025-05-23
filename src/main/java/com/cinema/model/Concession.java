package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "concessions")
public class Concession {
    @Id
    private String id;
    
    private String name;
    private String category;
    private String description;
    private Long price;
    private String image;
    private Boolean availability;
    private List<String> cinemaIds;
}