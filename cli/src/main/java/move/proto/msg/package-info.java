/** */
@ImportPackage(
    messages = {CreateDeliveryScheduleProfileApi.Request.class},
    actions = {
      @ImportAction(
          request = CreateDeliveryScheduleProfileApi.Request.class,
          response = CreateDeliveryScheduleProfileApi.Request.class)
    })
package move.proto.msg;

import com.movemedical.server.app.action.admin.CreateDeliveryScheduleProfileApi;
import run.mojo.annotations.ImportAction;
import run.mojo.annotations.ImportPackage;
