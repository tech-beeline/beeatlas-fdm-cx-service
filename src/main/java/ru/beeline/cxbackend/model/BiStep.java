package ru.beeline.cxbackend.model;

import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiStep {
    public String type;
    public String id;
    public String name;
}
