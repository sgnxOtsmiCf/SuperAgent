package cn.sgnxotsmicf.app.manus;
import cn.sgnxotsmicf.app.manus.model.AgentContext;
import cn.sgnxotsmicf.app.manus.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think(AgentContext context);

    public abstract String act(AgentContext context);

    @Override
    public String step(AgentContext context) {
        try {
            boolean shouldAct = think(context);
            if (!shouldAct) {
                context.setState(AgentState.FINISHED);
                return "思考完成 - 无需行动";
            }
            return act(context);
        } catch (Exception e) {
            log.error(e.getMessage());
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
