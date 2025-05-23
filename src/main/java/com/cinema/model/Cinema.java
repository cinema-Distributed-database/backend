package com.cinema.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cinemas")
public class Cinema {
    @Id
    private String id;
    
    private String name;
    private String address;
    private String city;
    private String image;
    
    private GeoJsonPoint location;
    
    private Contact contact;
    private List<String> amenities;
    private String status;
    private Integer roomCount;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private String phone;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoJsonPoint {
        private String type;
        private Double[] coordinates; // [longitude, latitude] [kinh độ, vĩ độ]
    }
}