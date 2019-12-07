package com.swmansion.reanimated.nodes;

import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.swmansion.reanimated.NodesManager;

import java.util.ArrayList;

public class DebugNode extends Node implements ValueManagingNode {

  public static final String TAG = "REANIMATED";
  private final String mMessage;
  private final int mValueID;

  public DebugNode(int nodeID, ReadableMap config, NodesManager nodesManager) {
    super(nodeID, config, nodesManager);
    mMessage = config.getString("message");
    mValueID = config.getInt("value");
  }

  @Override
  protected Object evaluate() {
    Object value = mNodesManager.findNodeById(mValueID, Node.class).value();
    Log.d(TAG, String.format("%s %s", mMessage, value));
    return value;
  }

  @Override
  public void setValue(Object value, ArrayList<CallFuncNode> context) {
    Node node = mNodesManager.findNodeById(mValueID, Node.class);
    ((ValueManagingNode) node).setValue(value, context);
  }
}
