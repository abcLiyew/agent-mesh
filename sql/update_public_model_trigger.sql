-- 修改智能体模型所有权检查触发器
-- 允许使用 user_id = 1 的公共模型

CREATE OR REPLACE FUNCTION check_agent_model_ownership() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    -- 验证 decision_model_id
    IF NEW.decision_model_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM ai_model
            WHERE id = NEW.decision_model_id 
            AND (user_id = NEW.user_id OR user_id = 1)  -- 用户自己的模型 或 公共模型(user_id=1)
            AND is_delete = 0
        ) THEN
            RAISE EXCEPTION 'Security Error: Decision model ID % does not belong to user % or is not a public model.', NEW.decision_model_id, NEW.user_id;
        END IF;
    END IF;

    -- 验证 response_model_id
    IF NEW.response_model_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM ai_model
            WHERE id = NEW.response_model_id 
            AND (user_id = NEW.user_id OR user_id = 1)  -- 用户自己的模型 或 公共模型(user_id=1)
            AND is_delete = 0
        ) THEN
            RAISE EXCEPTION 'Security Error: Response model ID % does not belong to user % or is not a public model.', NEW.response_model_id, NEW.user_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- 重新创建触发器（如果已存在则先删除）
DROP TRIGGER IF EXISTS trg_check_agent_model_ownership ON agent;
CREATE TRIGGER trg_check_agent_model_ownership
    BEFORE INSERT OR UPDATE ON agent
    FOR EACH ROW
    EXECUTE FUNCTION check_agent_model_ownership();

COMMENT ON FUNCTION check_agent_model_ownership() IS '验证智能体使用的模型是否属于当前用户或是公共模型(user_id=1)';
