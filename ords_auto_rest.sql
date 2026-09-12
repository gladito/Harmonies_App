BEGIN
    -- Enable Auto-REST for each table. 
    -- 'p_object' is the actual name of your table
    -- 'p_object_alias' is how it will be called in the URL (e.g., /ords/your_workspace/users/)
    -- p_auto_rest_auth is FALSE to avoid 403 errors while you test.
    
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'USERS', p_object_type => 'TABLE', p_object_alias => 'users', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'PROFILES', p_object_type => 'TABLE', p_object_alias => 'profiles', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'ROLES', p_object_type => 'TABLE', p_object_alias => 'roles', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'USER_ROLES', p_object_type => 'TABLE', p_object_alias => 'user_roles', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'CONTENTS', p_object_type => 'TABLE', p_object_alias => 'contents', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'POSTS', p_object_type => 'TABLE', p_object_alias => 'posts', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'TAGS', p_object_type => 'TABLE', p_object_alias => 'tags', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'POST_TAGS', p_object_type => 'TABLE', p_object_alias => 'post_tags', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'COMMENTS', p_object_type => 'TABLE', p_object_alias => 'comments', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'GROUPS', p_object_type => 'TABLE', p_object_alias => 'groups', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'GROUP_MEMBERS', p_object_type => 'TABLE', p_object_alias => 'group_members', p_auto_rest_auth => FALSE);
    ORDS.ENABLE_OBJECT(p_enabled => TRUE, p_schema => USER, p_object => 'GROUP_JOIN_REQUESTS', p_object_type => 'TABLE', p_object_alias => 'group_join_requests', p_auto_rest_auth => FALSE);

    COMMIT;
END;
/
