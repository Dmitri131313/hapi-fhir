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
package ca.uhn.fhir.jpa.entity;

import ca.uhn.fhir.jpa.model.entity.BasePartitionable;
import ca.uhn.fhir.jpa.model.entity.IdAndPartitionId;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.util.ValidateUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.length;

@Table(
		name = "TRM_CODESYSTEM_VER",
		// Note, we used to have a constraint named IDX_CSV_RESOURCEPID_AND_VER (don't reuse this)
		uniqueConstraints = {
			@UniqueConstraint(
					name = TermCodeSystemVersion.IDX_CODESYSTEM_AND_VER,
					columnNames = {"CODESYSTEM_PID", "CS_VERSION_ID"})
		},
		indexes = {
			@Index(name = "FK_CODESYSVER_RES_ID", columnList = "RES_ID"),
			@Index(name = "FK_CODESYSVER_CS_ID", columnList = "CODESYSTEM_PID")
		})
@Entity()
@IdClass(IdAndPartitionId.class)
public class TermCodeSystemVersion extends BasePartitionable implements Serializable {
	public static final String IDX_CODESYSTEM_AND_VER = "IDX_CODESYSTEM_AND_VER";
	public static final int MAX_VERSION_LENGTH = 200;
	private static final long serialVersionUID = 1L;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "myCodeSystem")
	private Collection<TermConcept> myConcepts;

	@Id
	@SequenceGenerator(name = "SEQ_CODESYSTEMVER_PID", sequenceName = "SEQ_CODESYSTEMVER_PID")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_CODESYSTEMVER_PID")
	@Column(name = "PID")
	private Long myId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns(
			value = {
				@JoinColumn(
						name = "RES_ID",
						referencedColumnName = "RES_ID",
						nullable = false,
						insertable = false,
						updatable = false),
				//				@JoinColumn(
				//						name = "PARTITION_ID",
				//						referencedColumnName = "PARTITION_ID",
				//						nullable = false,
				//						insertable = false,
				//						updatable = false)
			},
			foreignKey = @ForeignKey(name = "FK_CODESYSVER_RES_ID"))
	private ResourceTable myResource;

	@Column(name = "RES_ID", nullable = false)
	private Long myResourcePid;

	@Column(name = "CS_VERSION_ID", nullable = true, updatable = true, length = MAX_VERSION_LENGTH)
	private String myCodeSystemVersionId;

	/**
	 * This was added in HAPI FHIR 3.3.0 and is nullable just to avoid migration
	 * issued. It should be made non-nullable at some point.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns(
			value = {
				@JoinColumn(
						name = "CODESYSTEM_PID",
						referencedColumnName = "PID",
						insertable = false,
						updatable = false,
						nullable = true),
				//				@JoinColumn(
				//						name = "PARTITION_ID",
				//						referencedColumnName = "PARTITION_ID",
				//						insertable = false,
				//						nullable = true,
				//						updatable = false)
			},
			foreignKey = @ForeignKey(name = "FK_CODESYSVER_CS_ID"))
	private TermCodeSystem myCodeSystem;

	@Column(name = "CODESYSTEM_PID", insertable = true, updatable = true, nullable = true)
	private Long myCodeSystemPid;

	@Column(name = "CS_DISPLAY", nullable = true, updatable = true, length = MAX_VERSION_LENGTH)
	private String myCodeSystemDisplayName;

	/**
	 * Constructor
	 */
	public TermCodeSystemVersion() {
		super();
	}

	public TermCodeSystem getCodeSystem() {
		return myCodeSystem;
	}

	public TermCodeSystemVersion setCodeSystem(TermCodeSystem theCodeSystem) {
		myCodeSystem = theCodeSystem;
		myCodeSystemPid = theCodeSystem.getPid();
		assert myCodeSystemPid != null;
		return this;
	}

	public String getCodeSystemVersionId() {
		return myCodeSystemVersionId;
	}

	public TermCodeSystemVersion setCodeSystemVersionId(String theCodeSystemVersionId) {
		ValidateUtil.isNotTooLongOrThrowIllegalArgument(
				theCodeSystemVersionId,
				MAX_VERSION_LENGTH,
				"Version ID exceeds maximum length (" + MAX_VERSION_LENGTH + "): " + length(theCodeSystemVersionId));
		myCodeSystemVersionId = theCodeSystemVersionId;
		return this;
	}

	public Collection<TermConcept> getConcepts() {
		if (myConcepts == null) {
			myConcepts = new ArrayList<>();
		}
		return myConcepts;
	}

	@Nullable
	public Long getPid() {
		return myId;
	}

	@Nonnull
	public IdAndPartitionId getId() {
		return IdAndPartitionId.forId(myId, this);
	}

	public ResourceTable getResource() {
		return myResource;
	}

	public TermCodeSystemVersion setResource(ResourceTable theResource) {
		myResource = theResource;
		myResourcePid = theResource.getId().getId();
		setPartitionId(theResource.getPartitionId());
		return this;
	}

	public TermCodeSystemVersion setId(Long theId) {
		myId = theId;
		return this;
	}

	@Override
	public boolean equals(Object theO) {
		if (this == theO) {
			return true;
		}

		if (theO == null || getClass() != theO.getClass()) {
			return false;
		}

		TermCodeSystemVersion that = (TermCodeSystemVersion) theO;

		return new EqualsBuilder()
				.append(myCodeSystemVersionId, that.myCodeSystemVersionId)
				.append(myCodeSystemPid, that.myCodeSystemPid)
				.isEquals();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(myCodeSystemVersionId);
		b.append(myCodeSystemPid);
		return b.toHashCode();
	}

	public String getCodeSystemDisplayName() {
		return myCodeSystemDisplayName;
	}

	public void setCodeSystemDisplayName(String theCodeSystemDisplayName) {
		ValidateUtil.isNotTooLongOrThrowIllegalArgument(
				theCodeSystemDisplayName,
				MAX_VERSION_LENGTH,
				"Version ID exceeds maximum length (" + MAX_VERSION_LENGTH + "): " + length(theCodeSystemDisplayName));
		myCodeSystemDisplayName = theCodeSystemDisplayName;
	}

	public TermConcept addConcept() {
		TermConcept concept = new TermConcept();
		concept.setCodeSystemVersion(this);
		getConcepts().add(concept);
		return concept;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		b.append("pid", myId);
		b.append("displayName", myCodeSystemDisplayName);
		b.append("codeSystemResourcePid", myResourcePid);
		b.append("codeSystemPid", myCodeSystemPid);
		b.append("codeSystemVersionId", myCodeSystemVersionId);
		return b.toString();
	}

	TermCodeSystemVersion setCodeSystemPidForUnitTest(long theCodeSystemPid) {
		myCodeSystemPid = theCodeSystemPid;
		return this;
	}
}
