package com.dtos;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {

    private Long id;

    private String name;

    private String surname;

    private String email;

    private LocalDate birthDate;

    private Boolean active;
}
