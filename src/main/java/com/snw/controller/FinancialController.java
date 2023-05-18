package com.snw.controller;

import com.snw.controller.dto.FinancialDto;
import com.snw.controller.mapper.FinancialMapper;
import com.snw.controller.model.FinancialModel;
import com.snw.domain.FinancialEntity;
import com.snw.service.FinancialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FinancialController {

    private final FinancialMapper mapper;

    private final FinancialService service;

    @Autowired
    public FinancialController(FinancialMapper mapper, FinancialService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @PostMapping("/v1/financial")
    public ResponseEntity<FinancialModel> create(@RequestBody FinancialDto dto) {

        if (dto.getId() != null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        FinancialEntity entity = mapper.toEntity(dto);
        entity = service.save(entity);
        FinancialModel model = mapper.toModel(entity);

        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

    @GetMapping("/v1/financial")
    public ResponseEntity<List<FinancialModel>> findAll() {
        List<FinancialEntity> entities = service.findAll();

        return new ResponseEntity<>(mapper.toModel(entities), HttpStatus.OK);
    }

    @PutMapping("v1/financial")
    public ResponseEntity<FinancialModel> update(@RequestBody FinancialDto dto) {

        if (dto.getId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        FinancialEntity entity = mapper.toEntity(dto);
        entity = service.update(entity);
        FinancialModel model = mapper.toModel(entity);

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @PutMapping("v1/financial/{id}")
    public ResponseEntity<FinancialModel> updateById
            (@PathVariable String id, @RequestBody FinancialDto dto) {

        if (id.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        FinancialEntity entity = mapper.toEntity(dto);
        entity = service.updateById(id, entity);
        FinancialModel model = mapper.toModel(entity);

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @PatchMapping("v1/financial/{id}")
    public ResponseEntity<FinancialModel> partialUpdate
            (@PathVariable String id, @RequestBody Map<String, Object> changes) {

        if (id.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        FinancialEntity entity = service.partialUpdate(id, changes);
        FinancialModel model = mapper.toModel(entity);

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @DeleteMapping("v1/financial/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("v1/financial/{id}")
    public ResponseEntity<FinancialModel> getById(@PathVariable String id) {
        FinancialEntity entity = service.getById(id);
        FinancialModel model = mapper.toModel(entity);

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @GetMapping("v1/financial/user/{user}")
    public ResponseEntity<List<FinancialModel>> getByUserId(@PathVariable String user) {
        List<FinancialEntity> entities = service.getByUserId(user);
        List<FinancialModel> models = mapper.toModel(entities);

        return new ResponseEntity<>(models, HttpStatus.OK);
    }

    /*
     * TODO: PART 1: add RESTful API
     * create: •done
     * update: •done
     * partial update: •done
     * delete: •done
     * find by id: •done
     * find by user id: •done
     * */


}
