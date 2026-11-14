package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;

import java.util.List;

public interface BasicService<I,O> {

    void create(final I request);
    void update(final String id ,final I request);
    O findById(final String id);
    void delete(final String id);
    PageResponse<O> findAll(final int page, final int size);
  //  List<O> findAll();
}
