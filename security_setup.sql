-- Harmonyapp Security and Privileges Script
-- This script sets up ORDS roles and privileges to secure the REST services.

BEGIN
    -- 1. Create ORDS Role
    ORDS.CREATE_ROLE(p_role_name => 'harmony_api_role');

    -- 2. Create ORDS Privilege
    -- This privilege will protect the /harmony/ base path
    ORDS.DEFINE_PRIVILEGE(
        p_privilege_name => 'harmony_privilege',
        p_roles          => ora_sxtab('harmony_api_role'),
        p_patterns       => ora_sxtab('/harmony/*'),
        p_label          => 'Harmonyapp API Privilege',
        p_description    => 'Protects all Harmonyapp REST endpoints');

    -- 3. Map Privilege to the Module
    -- Although patterns cover it, explicit mapping is a Good Practice
    ORDS.CREATE_PRIVILEGE_MAPPING(
        p_privilege_name => 'harmony_privilege',
        p_patterns       => ora_sxtab('/harmony/*'));

    COMMIT;
END;
/

-- NOTE TO USER: 
-- To access the secured endpoints, you must:
-- 1. Create an OAuth Client in APEX (SQL Workshop > RESTful Services > OAuth Clients).
-- 2. Assign the 'harmony_api_role' to that client.
-- 3. Use the Client ID and Client Secret to get a Bearer Token.
