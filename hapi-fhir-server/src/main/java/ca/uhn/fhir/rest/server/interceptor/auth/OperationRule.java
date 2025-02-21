/*
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.rest.server.interceptor.auth;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor.Verdict;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class OperationRule extends BaseRule implements IAuthRule {
	private String myOperationName;
	private boolean myAppliesToServer;
	private HashSet<Class<? extends IBaseResource>> myAppliesToTypes;
	private List<IIdType> myAppliesToIds;
	private HashSet<Class<? extends IBaseResource>> myAppliesToInstancesOfType;
	private boolean myAppliesToAnyType;
	private boolean myAppliesToAnyInstance;
	private boolean myAppliesAtAnyLevel;
	private boolean myAllowAllResponses;
	private boolean myAllowAllResourcesAccess;

	OperationRule(String theRuleName) {
		super(theRuleName);
	}

	void appliesAtAnyLevel(boolean theAppliesAtAnyLevel) {
		myAppliesAtAnyLevel = theAppliesAtAnyLevel;
	}

	public void allowAllResponses() {
		myAllowAllResponses = true;
	}

	public void allowAllResourcesAccess() {
		myAllowAllResourcesAccess = true;
	}

	void appliesToAnyInstance() {
		myAppliesToAnyInstance = true;
	}

	void appliesToAnyType() {
		myAppliesToAnyType = true;
	}

	void appliesToInstances(List<IIdType> theAppliesToIds) {
		myAppliesToIds = theAppliesToIds;
	}

	void appliesToInstancesOfType(HashSet<Class<? extends IBaseResource>> theAppliesToTypes) {
		myAppliesToInstancesOfType = theAppliesToTypes;
	}

	void appliesToServer() {
		myAppliesToServer = true;
	}

	void appliesToTypes(HashSet<Class<? extends IBaseResource>> theAppliesToTypes) {
		myAppliesToTypes = theAppliesToTypes;
	}

	@SuppressWarnings("EnumSwitchStatementWhichMissesCases")
	@Override
	public Verdict applyRule(
			RestOperationTypeEnum theOperation,
			RequestDetails theRequestDetails,
			IBaseResource theInputResource,
			IIdType theInputResourceId,
			IBaseResource theOutputResource,
			IRuleApplier theRuleApplier,
			Set<AuthorizationFlagsEnum> theFlags,
			Pointcut thePointcut) {
		FhirContext ctx = theRequestDetails.getServer().getFhirContext();

		boolean applies = false;
		switch (theOperation) {
			case EXTENDED_OPERATION_SERVER:
				if (myAppliesToServer || myAppliesAtAnyLevel) {
					applies = true;
				}
				break;
			case EXTENDED_OPERATION_TYPE:
				if (myAppliesToAnyType || myAppliesAtAnyLevel) {
					applies = true;
				} else if (myAppliesToTypes != null) {
					// TODO: Convert to a map of strings and keep the result
					for (Class<? extends IBaseResource> next : myAppliesToTypes) {
						String resName = ctx.getResourceType(next);
						if (resName.equals(theRequestDetails.getResourceName())) {
							applies = true;
							break;
						}
					}
				}
				break;
			case EXTENDED_OPERATION_INSTANCE:
				if (myAppliesToAnyInstance || myAppliesAtAnyLevel) {
					applies = true;
				} else {
					IIdType requestResourceId = null;
					if (theRequestDetails.getId() != null) {
						requestResourceId = theRequestDetails.getId();
					}
					if (requestResourceId != null) {
						if (myAppliesToIds != null) {
							String instanceId =
									requestResourceId.toUnqualifiedVersionless().getValue();
							for (IIdType next : myAppliesToIds) {
								if (next.toUnqualifiedVersionless().getValue().equals(instanceId)) {
									applies = true;
									break;
								}
							}
						}
						if (myAppliesToInstancesOfType != null) {
							// TODO: Convert to a map of strings and keep the result
							for (Class<? extends IBaseResource> next : myAppliesToInstancesOfType) {
								String resName = ctx.getResourceType(next);
								if (resName.equals(requestResourceId.getResourceType())) {
									applies = true;
									break;
								}
							}
						}
					}
				}
				break;
			default:
				return null;
		}

		if (!applies) {
			return null;
		}

		if (myOperationName != null && !myOperationName.equals(theRequestDetails.getOperation())) {
			return null;
		}

		if (theOutputResource == null) {
			// This is the request part
			return newVerdict(
					theOperation,
					theRequestDetails,
					theInputResource,
					theInputResourceId,
					theOutputResource,
					theRuleApplier);
		} else {
			// This is the response part, so we might want to check all of the
			// resources in the response
			if (myAllowAllResponses) {
				return newVerdict(
						theOperation,
						theRequestDetails,
						theInputResource,
						theInputResourceId,
						theOutputResource,
						theRuleApplier);
			} else {
				List<IBaseResource> outputResources = AuthorizationInterceptor.toListOfResourcesAndExcludeContainer(
						theOutputResource, theRequestDetails.getFhirContext());
				return RuleImplOp.applyRulesToResponseResources(
						theRequestDetails, theRuleApplier, thePointcut, outputResources);
			}
		}
	}

	/**
	 * Must include the leading $
	 */
	public void setOperationName(String theOperationName) {
		myOperationName = theOperationName;
	}

	String getOperationName() {
		return myOperationName;
	}

	boolean isAppliesToServer() {
		return myAppliesToServer;
	}

	HashSet<Class<? extends IBaseResource>> getAppliesToTypes() {
		return myAppliesToTypes;
	}

	List<IIdType> getAppliesToIds() {
		return myAppliesToIds;
	}

	HashSet<Class<? extends IBaseResource>> getAppliesToInstancesOfType() {
		return myAppliesToInstancesOfType;
	}

	boolean isAppliesToAnyType() {
		return myAppliesToAnyType;
	}

	boolean isAppliesToAnyInstance() {
		return myAppliesToAnyInstance;
	}

	boolean isAppliesAtAnyLevel() {
		return myAppliesAtAnyLevel;
	}

	boolean isAllowAllResponses() {
		return myAllowAllResponses;
	}

	boolean isAllowAllResourcesAccess() {
		return myAllowAllResourcesAccess;
	}

	@Override
	@Nonnull
	protected ToStringBuilder toStringBuilder() {
		ToStringBuilder builder = super.toStringBuilder();
		builder.append("op", myOperationName);
		builder.append("appliesToServer", myAppliesToServer);
		builder.append("appliesToTypes", myAppliesToTypes);
		builder.append("appliesToIds", myAppliesToIds);
		builder.append("appliesToInstancesOfType", myAppliesToInstancesOfType);
		builder.append("appliesToAnyType", myAppliesToAnyType);
		builder.append("appliesToAnyInstance", myAppliesToAnyInstance);
		builder.append("appliesAtAnyLevel", myAppliesAtAnyLevel);
		builder.append("allowAllResponses", myAllowAllResponses);
		builder.append("allowAllResourcesAccess", myAllowAllResourcesAccess);
		return builder;
	}
}
