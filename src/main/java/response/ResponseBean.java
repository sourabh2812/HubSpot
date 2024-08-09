package response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ResponseBean {
    private int userId;
    private List<Integer> viewableDealIds;
    private List<Integer> editableDealIds;
}