package com.linkedin.clustermanager;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.linkedin.clustermanager.Mocks.MockCMTaskExecutor;
import com.linkedin.clustermanager.Mocks.MockManager;
import com.linkedin.clustermanager.Mocks.MockStateModel;
import com.linkedin.clustermanager.messaging.handling.AsyncCallbackService;
import com.linkedin.clustermanager.messaging.handling.CMStateTransitionHandler;
import com.linkedin.clustermanager.model.Message;
import com.linkedin.clustermanager.model.Message.MessageType;

public class TestCMTaskExecutor
{

  @Test (groups = {"unitTest"})
  public void testCMTaskExecutor() throws Exception
  {
    System.out.println("TestCMTaskHandler.testInvocation()");
    String msgId = "TestMessageId";
    Message message = new Message(MessageType.TASK_REPLY,msgId);
   
    message.setMsgId(msgId);
    message.setSrcName("cm-instance-0");
    message.setTgtName("cm-instance-1");
    message.setTgtSessionId("1234");
    message.setFromState("Offline");
    message.setToState("Slave");
    message.setStateUnitKey("Teststateunitkey");
    message.setStateUnitGroup("Teststateunitkey");
    
    MockCMTaskExecutor executor = new MockCMTaskExecutor();
    MockStateModel stateModel = new MockStateModel();
    NotificationContext context;
    executor.registerMessageHandlerFactory(
        MessageType.TASK_REPLY.toString(), new AsyncCallbackService());
    String clusterName =" testcluster";
    context = new NotificationContext(new MockManager(clusterName));
    CMStateTransitionHandler handler = new CMStateTransitionHandler(stateModel);
    executor.scheduleTask(message, handler, context);
    while (!executor.isDone(msgId))
    {
      Thread.sleep(500);
    }
    AssertJUnit.assertTrue(stateModel.stateModelInvoked);
  }

}
