package org.osc.core.broker.service.dto;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.util.ValidateUtil;

import javax.validation.constraints.NotNull;


public class BaseDto {

    @NotNull(message = "id is required")
    private Long id;
    private Long parentId;

    public BaseDto() {
    }

    public BaseDto(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public static void checkForNullId(BaseDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Id", dto.getId());

        ValidateUtil.checkForNullFields(map);
    }

}
