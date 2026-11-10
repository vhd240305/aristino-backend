package com.aristino.backend.controller;

import com.aristino.backend.dto.address.AddressRequest;
import com.aristino.backend.dto.address.AddressResponse;
import com.aristino.backend.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // GET /api/addresses/me - lay so dia chi cua chinh user dang dang nhap
    @GetMapping("/me")
    public List<AddressResponse> getMyAddresses() {
        return addressService.getMyAddresses();
    }

    // POST /api/addresses - them dia chi moi vao so dia chi
    @PostMapping
    public AddressResponse create(@Valid @RequestBody AddressRequest request) {
        return addressService.create(request);
    }

    // PUT /api/addresses/{id} - sua dia chi
    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return addressService.update(id, request);
    }

    // PUT /api/addresses/{id}/default - dat lam dia chi mac dinh
    @PutMapping("/{id}/default")
    public AddressResponse setDefault(@PathVariable Long id) {
        return addressService.setDefault(id);
    }

    // DELETE /api/addresses/{id} - xoa dia chi
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        addressService.delete(id);
    }
}