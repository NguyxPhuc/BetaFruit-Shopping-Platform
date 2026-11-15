package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.CODCollection;
import com.fu.SWP391_BetaFruit.entity.CODDetail;
import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import com.fu.SWP391_BetaFruit.entity.Shipper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CODCollectionService {
    Page<CODCollection> getCollectionsPage(Integer shipperId, Pageable pageable);

    List<CODCollection> getCollectionsByShipper(Integer shipperId);

    CODCollection getCollectionById(Integer collectionId);

    List<CODDetail> getCollectionDetails(Integer collectionId);

    List<DeliveryAssignment> getUnremittedAssignmentsByShipper(Integer shipperId);

    List<Shipper> getShippersWithDebt();

    List<Shipper> getAllShippers();

    BigDecimal getShipperDebt(Integer shipperId);

    CODCollection remitCodCollection(Integer staffId, Integer shipperId, List<Integer> orderIds);
}
