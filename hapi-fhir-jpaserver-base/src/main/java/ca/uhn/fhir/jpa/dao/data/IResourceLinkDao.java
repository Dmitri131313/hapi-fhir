/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.dao.data;

import ca.uhn.fhir.jpa.model.dao.JpaPid;
import ca.uhn.fhir.jpa.model.entity.ResourceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IResourceLinkDao extends JpaRepository<ResourceLink, Long>, IHapiFhirJpaRepository {

	@Modifying
	@Query("DELETE FROM ResourceLink t WHERE t.mySourceResource.myPid = :resId")
	void deleteByResourceId(@Param("resId") JpaPid theResourcePid);

	@Query("SELECT t FROM ResourceLink t WHERE t.mySourceResource.myPid = :resId")
	List<ResourceLink> findAllForSourceResourceId(@Param("resId") JpaPid thePatientId);

	@Query("SELECT t FROM ResourceLink t WHERE t.myTargetResource.myPid in :resIds")
	List<ResourceLink> findWithTargetPidIn(@Param("resIds") List<JpaPid> thePids);

	/**
	 * Loads a collection of ResourceLink entities by PID, but also  eagerly fetches
	 * the target resources and their forced IDs
	 */
	@Query("SELECT t FROM ResourceLink t LEFT JOIN FETCH t.myTargetResource tr WHERE t.myId in :pids")
	List<ResourceLink> findByPidAndFetchTargetDetails(@Param("pids") List<Long> thePids);
}
