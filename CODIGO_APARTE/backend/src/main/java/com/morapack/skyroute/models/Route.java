package com.morapack.skyroute.models;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "route")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private int quantity;
    private Duration slack = Duration.ZERO;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "route_id" , referencedColumnName = "id") // FK en route_segment
    private List<RouteSegment> segments = new ArrayList<>();

    public void add(RouteSegment segment) {
        segments.add(segment);
    }

    public Route(int quantity){
        this.quantity = quantity;
    }
    public Route(){
        this.quantity = 0;
    }

    public void releaseResources() {
        for (RouteSegment segment : segments) {
            segment.release();
        }
    }

        public void setSlack(Duration slack) {
        this.slack = slack;
    }
}