package move.proto.api.mobile.v1;

import com.movemedical.server.app.action.admin.CreateDeliveryScheduleProfileApi;
import io.grpc.MethodDescriptor;
import run.mojo.annotations.ModuleName;
import run.mojo.annotations.ImportAction;
import run.mojo.annotations.ImportPackage;

/** */
@ImportPackage(
    /** Force include messages. */
    messages = {CreateDeliveryScheduleProfileApi.Request.class},
    /** Actions. */
    actions = {
      @ImportAction(
          request = CreateDeliveryScheduleProfileApi.Request.class,
          response = CreateDeliveryScheduleProfileApi.Response.class,
          fullName = "somepath/MyService",
          type = MethodDescriptor.MethodType.UNKNOWN)
    })
@ModuleName("INTERNAL")
public class com_movemedical_server_app_action_admin {}
