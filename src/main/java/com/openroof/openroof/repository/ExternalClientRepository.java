package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.ExternalClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface ExternalClientRepository extends JpaRepository<ExternalClient, Long>, JpaSpecificationExecutor<ExternalClient> {
    
    @Query(value = """
            SELECT * FROM (
                SELECT 
                    ac.id as id, 
                    u.id as user_id, 
                    u.name as name, 
                    u.email as email, 
                    u.phone as phone, 
                    ac.status as status, 
                    ac.priority as priority, 
                    ac.client_type as client_type, 
                    ac.last_contact_date as last_contact_date, 
                    ac.created_at as created_at, 
                    'AGENT' as internal_type
                FROM agent_clients ac
                JOIN users u ON ac.user_id = u.id
                WHERE ac.agent_id = :agentId AND ac.deleted_at IS NULL
                
                UNION ALL
                
                SELECT 
                    ec.id as id, 
                    NULL as user_id, 
                    ec.name as name, 
                    ec.email as email, 
                    ec.phone as phone, 
                    ec.status as status, 
                    ec.priority as priority, 
                    ec.client_type as client_type, 
                    ec.last_contact_date as last_contact_date, 
                    ec.created_at as created_at, 
                    'EXTERNAL' as internal_type
                FROM external_clients ec
                WHERE ec.agent_id = :agentId AND ec.deleted_at IS NULL
            ) as unified_clients
            WHERE (:query IS NULL OR :query = '' OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(email) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:status IS NULL OR status = :status)
            AND (:clientType IS NULL OR client_type = :clientType)
            AND (:internalType IS NULL OR internal_type = :internalType)
            """, 
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT ac.id FROM agent_clients ac WHERE ac.agent_id = :agentId AND ac.deleted_at IS NULL
                UNION ALL
                SELECT ec.id FROM external_clients ec WHERE ec.agent_id = :agentId AND ec.deleted_at IS NULL
            ) as unified_counts
            WHERE (:internalType IS NULL OR CASE 
                WHEN EXISTS (SELECT 1 FROM agent_clients WHERE id = unified_counts.id) THEN 'AGENT' 
                ELSE 'EXTERNAL' END = :internalType)
            """,
            nativeQuery = true)
    Page<Map<String, Object>> searchUnifiedClients(
            @Param("agentId") Long agentId,
            @Param("query") String query,
            @Param("status") String status,
            @Param("clientType") String clientType,
            @Param("internalType") String internalType,
            Pageable pageable);
}
