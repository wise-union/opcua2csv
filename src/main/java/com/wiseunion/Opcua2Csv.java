package com.wiseunion;

import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;


public class Opcua2Csv {

    private static List<NodeInfo> nodeInfoList = new ArrayList<>();
    private static String endpointUrl = "";
    private static String isVariableOnly = "0";

    public static void main(String[] args) throws Exception,UaException {
        // 创建一个OPC UA客户端
        endpointUrl = args.length==0? "opc.tcp://localhost:48020" : args[0];
        isVariableOnly = args.length==0? "0" : args[1];

        // System.out.println(endpointUrl + "  :  " + isVariableOnly);
        
        // OpcUaClient client = OpcUaClient.create("opc.tcp://LAPTOP-VI4T7MK9:48020");
        OpcUaClient client = OpcUaClient.create(endpointUrl);
        // 连接到OPC UA服务器
        client.connect().get();

        browseNode("", client, Identifiers.RootFolder);

        // 导出nodeInfoList到CSV文件
        exportNodeInfoListToCsv("node_info.csv");
    }

    // public static Boolean checkDataType(OpcUaClient opcUaClient, NodeId nodeId, Object value) throws UaException {
    public static String getNodeDataType(OpcUaClient opcUaClient, NodeId nodeId) throws UaException {
        NodeId typeNodeId = opcUaClient.getAddressSpace().getVariableNode(nodeId).getDataType();
        BuiltinDataType typeObj = BuiltinDataType.fromNodeId(typeNodeId);
        String type = "";
        if (typeObj == null) {
            UaNode typeNode = opcUaClient.getAddressSpace().getNode(typeNodeId);
            type = typeNode.getBrowseName().getName();
        }
        else {
            type = typeObj.name();
        }
        return type;
    }

    private static void exportNodeInfoListToCsv(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // 写入CSV文件头部
            writer.append("Node Path,Node Address,Access Level,Data Type\n");

            // 写入每个节点的信息
            for (NodeInfo nodeInfo : nodeInfoList) {
                writer.append(nodeInfo.getNodePath()).append(",");
                writer.append(nodeInfo.getNodeAddress()).append(",");
                writer.append(nodeInfo.getAccessLevel()).append(",");
                writer.append(nodeInfo.getDataType()).append("\n");
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        BrowseDescription browse = new BrowseDescription(
            browseRoot,
            BrowseDirection.Forward,
            Identifiers.References,
            true,
            uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue() | NodeClass.Method.getValue()),
            uint(BrowseResultMask.All.getValue())
        );

        try {
            BrowseResult browseResult = client.browse(browse).get();

            List<ReferenceDescription> references = toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                String nodePath = indent + "/" + rd.getBrowseName().getName();
                String nodeAddress = "n="+rd.getNodeId().getNamespaceIndex() + ";";
                String accessLevel = "";
                String dataType = "";
                String nodeClass = rd.getNodeClass().toString();

                if (nodeClass.equals("Variable")) {
                    try {
                        ExpandedNodeId eNId = rd.getNodeId(); //rd.getReferenceTypeId(); //
                        Object identifier = eNId.getIdentifier();
                        NodeId nId = null;
                        if (identifier instanceof String) {
                            nodeAddress = nodeAddress + "s=" + rd.getNodeId().getIdentifier().toString();
                            nId = new NodeId(eNId.getNamespaceIndex().intValue(), (String)identifier);
                        } else if (identifier instanceof UInteger) {
                            nodeAddress = nodeAddress + "i=" + rd.getNodeId().getIdentifier().toString();
                            nId = new NodeId(eNId.getNamespaceIndex().intValue(), (UInteger)identifier);
                        } else {
                            System.out.println("identifier type error: " + nodePath);
                        }
                        UaVariableNode node = client.getAddressSpace().getVariableNode(nId);

                        String[] accessLevelMap = {
                            "none", 
                            "currentRead", 
                            "currentWrite", 
                            "currentRead + currentWrite",
                            "HistoryRead",
                            "5",
                            "6",
                            "7",
                            "HistoryWrite",
                            "9",
                            "10",
                            "11",
                            "12",
                            "13",
                            "14",
                            "15",
                            "SemanticChange",
                            "17"
                        };

                        int accessLevelValue = node.getAccessLevel().intValue();
                        if (accessLevelValue < accessLevelMap.length) {
                            accessLevel = accessLevelMap[node.getAccessLevel().intValue()];
                        }
                        else {
                            System.out.println("cannot find accessLevel: " + nodePath + "  accessLevelValue: " + accessLevelValue);
                            accessLevel = String.valueOf(accessLevelValue);
                        }
                        if (nId == null) {
                            continue;
                        }
                        dataType = getNodeDataType(client, nId);
                        if (dataType == null) {
                            System.out.println("dataType is null: " + nodePath);
                        }
                    } catch (UaException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                else {
                    // System.out.println("NodeClass: " + nodeClass);
                }
                
                if (isVariableOnly.equals("1") && !nodeClass.equals("Variable")) {
                    // 不保存
                }
                else {
                    // 创建NodeInfo对象并保存节点信息到nodeInfoList中
                    NodeInfo nodeInfo = new NodeInfo(nodePath, nodeAddress, accessLevel, dataType);
                    nodeInfoList.add(nodeInfo);
                }

                System.out.print(".");
                // recursively browse to children
                rd.getNodeId().toNodeId(client.getNamespaceTable())
                    .ifPresent(nodeId -> browseNode(nodePath, client, nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e);
        }
    }
    
    private static class NodeInfo {
        private String nodePath;
        private String nodeAddress;
        private String accessLevel;
        private String dataType;
        
        public NodeInfo(String nodePath, String nodeAddress, String accessLevel, String dataType) {
            this.nodePath = nodePath;
            this.nodeAddress = nodeAddress;
            this.accessLevel = accessLevel;
            this.dataType = dataType;
        }
        
        public String getNodePath() {
            return nodePath;
        }
        
        public String getNodeAddress() {
            return nodeAddress;
        }
        
        public String getAccessLevel() {
            return accessLevel;
        }
        
        public String getDataType() {
            return dataType;
        }
    }

}

