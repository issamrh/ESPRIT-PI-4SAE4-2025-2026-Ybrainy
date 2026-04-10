package com.esprit.demo.Services;

import com.esprit.demo.Dto.ThreadRequest;
import com.esprit.demo.Dto.ThreadResponse;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface ThreadService {

    public ThreadResponse create(ThreadRequest request) ;

    public List<ThreadResponse> getAll() ;

    public ThreadResponse getById(Long id);

    public List<ThreadResponse> getByCategory(Long categoryId);

    public List<ThreadResponse> getByAuthor(Long authorId);

    public ThreadResponse update(Long id, ThreadRequest request) throws BadRequestException;

    public void delete(Long id);

    public ThreadResponse lock(Long id) throws BadRequestException;

    public ThreadResponse unlock(Long id);

    public ThreadResponse close(Long id);

    public ThreadResponse reopen(Long id);




}












