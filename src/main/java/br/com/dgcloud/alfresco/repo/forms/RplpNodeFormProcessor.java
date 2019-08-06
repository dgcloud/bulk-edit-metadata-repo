package br.com.dgcloud.alfresco.repo.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormData.FieldData;
import org.alfresco.repo.forms.processor.node.NodeFormProcessor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;

/**
 * The node form processor is overridden in order to be able to meet the
 * requirement to update properties of multiple nodes at the same time. This
 * implementation will try to save every requested node, and collect info on
 * failed ones. If there was one failure an exception will be thrown after
 * successful persistance of the other ones in order to inform the user.
 * 
 * @author erik.billerby@redpill-linpro.com
 *
 */
public class RplpNodeFormProcessor extends NodeFormProcessor {
	private static final Logger logger = Logger.getLogger(RplpNodeFormProcessor.class);
	protected static String MULTIPLE_NODE_REFS_FIELD_NAME = "muliple-edit-nodeRefs";

	@Override
	protected NodeRef internalPersist(NodeRef item, FormData data) {

		List<String> nodeRefs = null;
		List<NodeRef> failedNodeRefs = new ArrayList<>();
		for (FieldData fieldData : data) {

			String fieldName = fieldData.getName();
			if (MULTIPLE_NODE_REFS_FIELD_NAME.equals(fieldName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("This is a call to update properties for multiple nodes at once.");
				}
				String value = (String) fieldData.getValue();
				nodeRefs = Arrays.asList(value.split("\\s*,\\s*"));
				// we found it, no need to continue the loop
				break;
			}
		}
		if (nodeRefs != null) {
			for (String nodeRefString : nodeRefs) {
				NodeRef nodeRef = new NodeRef(nodeRefString);
				try {
					super.internalPersist(nodeRef, data);
				} catch (Exception e) {
					failedNodeRefs.add(nodeRef);
				}
			}

			// After saving all nodes possible check if we got any error and throw exception
			// to inform user on which nodes we could not update.
			if (!failedNodeRefs.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (NodeRef failedNode : failedNodeRefs) {
					String name = (String) nodeService.getProperty(failedNode, ContentModel.PROP_NAME);
					sb.append(name).append(",");
				}
				throw new AlfrescoRuntimeException("rplp.exception.update-multiple-nodes.failedNodes",
						new String[] { sb.toString() });
			}
			return item;
		} else {
			// This is a normal single node call.
			return super.internalPersist(item, data);
		}

	}

}