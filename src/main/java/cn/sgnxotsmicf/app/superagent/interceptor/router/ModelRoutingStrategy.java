package cn.sgnxotsmicf.app.superagent.interceptor.router;


public interface ModelRoutingStrategy {
	    DynamicModelRouter.ModelTier decide(TaskProfile profile);
	}