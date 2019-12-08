package com.swmansion.reanimated.bridging;

import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.DynamicFromArray;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.UIManagerReanimatedHelper;
import com.facebook.react.uimanager.ViewManager;
import com.swmansion.reanimated.NodesManager;
import com.swmansion.reanimated.nodes.Node;
import com.swmansion.reanimated.nodes.ValueNode;

import java.util.Map;

import static com.swmansion.reanimated.Utils.concat;

public class ViewManagerAccessor implements ReanimatedBridge.ReanimatedAccessor {
    private final UIManagerModule mUIManager;
    private Dynamic mCommandId;
    private ViewManager mViewManager;
    private Map<String, Object> mCommandsMap;
    private View mView;
    private int mConnectedViewTag = View.NO_ID;
    private Boolean mAttachedToAnimatedView = false;

    ViewManagerAccessor(ReactContext context, String viewManagerName, Dynamic commandId){
        mUIManager = context.getNativeModule(UIManagerModule.class);
        setCaller(viewManagerName, commandId);
    }

    private void setCaller(String viewManagerName, Dynamic commandId) {
        resolveViewManager(viewManagerName);
        setCommand(commandId);
    }

    private void resolveViewManager(String name) {
        try {
            mViewManager = UIManagerReanimatedHelper
                    .resolveViewManager(mUIManager.getUIImplementation(), name);
            mCommandsMap = mViewManager.getCommandsMap();
        } catch (Throwable err){
            Map<String, ViewManager> viewManagers = null;
            try {
                viewManagers = ReanimatedViewManagerRegistry.getViewManagers(mUIManager.getUIImplementation());
            } catch (NoSuchFieldException e) {
                //  noop
            } catch (IllegalAccessException e) {
                //  noop
            }
            String[] keys = viewManagers.keySet().toArray(new String[viewManagers.size()]);
            String details = "Expected one of:\n" + concat(keys);

            throw new JSApplicationIllegalArgumentException(
                    "Animated invoke: View manager with name " + name + " was not found." + details,
                    err
            );
        }
    }

    private void setCommand(Dynamic commandId) {
        mCommandId = commandId;
        /*
          validate {@link mCommandId} against the {@link mCommandsMap} of the {@link ViewManager}
         */
        if (mCommandsMap != null && mCommandsMap.size() > 0){
            if(mCommandsMap.containsValue(mCommandId.getType() == ReadableType.String ? mCommandId.asString(): mCommandId.asInt())){
                // all is good
            } else if (mCommandsMap.containsKey(mCommandId.asString())){
                WritableArray temp = new WritableNativeArray();
                temp.pushString(mCommandId.asString());
                mCommandId.recycle();
                mCommandId = DynamicFromArray.create(temp, 0);
            }
            else {
                // View manager command was not found
                throw new JSApplicationIllegalArgumentException(
                        "Animated invoke: View manager command " + mCommandId.toString() + " was not found. Expected one of:\n" +
                                mCommandsMap.entrySet().toString()
                );
            }
        } else {
            String message = "Animated invoke: could not find commands map for View manager " + mViewManager.getClass().getSimpleName();

            //  display warning as it is not fatal
            Log.w(ReactConstants.TAG, message);
        }
    }

    private void setViewTag(int viewTag) {
        if(viewTag != mConnectedViewTag) {
            mConnectedViewTag = viewTag;
            mView = mUIManager.resolveView(mConnectedViewTag);
        }
    }

    @Override
    public void connectToView(int viewTag) {
        mAttachedToAnimatedView = true;
        setViewTag(viewTag);
    }

    @Override
    public void disconnectFromView(int viewTag) {
        mAttachedToAnimatedView = false;
        mConnectedViewTag = View.NO_ID;
        mView = null;
    }

    @Override
    public void call(int[] params, NodesManager nodesManager) {
        ReanimatedWritableNativeArray args = new ReanimatedWritableNativeArray();
        Node n;
        int paramStart;

        /*
          If this node isn't attached to a view the first param node must be the view's tag
         */
        if(mAttachedToAnimatedView) {
            paramStart = 0;
        } else {
            paramStart = 1;
            ValueNode tagValueNode = nodesManager.findNodeById(params[0], ValueNode.class);
            setViewTag(tagValueNode.doubleValue().intValue());
        }

        for (int i = paramStart; i < params.length; i++) {
            n = nodesManager.findNodeById(params[i], Node.class);
            Object value = n.value();

            if (value instanceof ReanimatedCallback) {
                /*
                  {@link ViewManager } has no {@link Callback} or {@link Promise} args
                 */
                throw new JSApplicationIllegalArgumentException(
                        "Parameter mismatch when calling reanimated invoke.\n" +
                                "Dispatch can't receive callback params, Param# " + i + 1
                );
            } else {
                args.pushDynamic(value);
            }
        }

        receiveCommand(args);
    }

    private void receiveCommand(ReadableArray args) {
        if (mCommandId.getType().equals(ReadableType.Number)) {
            mViewManager.receiveCommand(mView, mCommandId.asInt(), args);
        } else {
            mViewManager.receiveCommand(mView, mCommandId.asString(), args);
        }
    }
}
