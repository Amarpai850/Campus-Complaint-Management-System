package com.campus.complaint.repository;

import com.campus.complaint.entity.Complaint;
import com.campus.complaint.entity.ComplaintStatus;
import com.campus.complaint.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findByAssignedStaffOrderByCreatedAtDesc(User staff);

    List<Complaint> findByAssignedStaffIdOrderByCreatedAtDesc(Long staffId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    long countByStatus(ComplaintStatus status);

    @Query("SELECT c FROM Complaint c ORDER BY c.createdAt DESC")
    List<Complaint> findAllOrderByCreatedAtDesc();

    @Query("SELECT c FROM Complaint c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<Complaint> findByStatusOrderByCreatedAtDesc(@Param("status") ComplaintStatus status);

    @Query("SELECT c.category.name, COUNT(c) FROM Complaint c GROUP BY c.category.name")
    List<Object[]> countByCategory();
}
