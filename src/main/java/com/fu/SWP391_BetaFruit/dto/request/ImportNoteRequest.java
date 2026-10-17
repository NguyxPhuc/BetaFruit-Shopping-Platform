package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ImportNoteRequest {
    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất 1 sản phẩm")
    @Valid
    private List<ImportNoteDetailRequest> details = new ArrayList<>();
}
