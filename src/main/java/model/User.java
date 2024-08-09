package model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class User {
    private int userId;
    private List<Integer> teamIds;
    private String viewPermissionLevel;
    private String editPermissionLevel;
}