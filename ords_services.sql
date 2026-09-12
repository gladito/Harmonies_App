-- =================================================================================
-- HARMONYAPP: MANUAL ORDS RESTful SERVICES SCRIPT 
-- (Overrides the need for Auto-REST and Auto-Schema privileges)
-- 
-- This script creates a custom ORDS module, with its templates and handlers, 
-- specifically tailored for the Harmonyapp Spring Boot backend.
-- =================================================================================

BEGIN
  -- 1. Create the Module (The main base path for our API)
  -- URL will be: .../ords/your_workspace/harmony/...
  ORDS.DEFINE_MODULE(
      p_module_name    => 'harmony_api',
      p_base_path      => '/harmony/',
      p_items_per_page => 25,
      p_status         => 'PUBLISHED',
      p_comments       => 'Manual API Module for Harmonyapp Backend'
  );

  -- ==========================================
  -- USERS ENDPOINTS
  -- ==========================================

  -- 2. Define Template for USERS (Collection)
  -- URL: .../ords/your_workspace/harmony/users/
  ORDS.DEFINE_TEMPLATE(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/'
  );

  -- Handler: GET all users
  ORDS.DEFINE_HANDLER(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/',
      p_method         => 'GET',
      p_source_type    => 'json/collection',
      p_items_per_page => 25,
      p_mimes_allowed  => '',
      p_comments       => 'Get all users',
      p_source         => 'SELECT * FROM USERS WHERE DELETED = 0'
  );

  -- Handler: POST a new user
  -- Automatically handles JSON body based on column names.
  ORDS.DEFINE_HANDLER(
    p_module_name    => 'harmony_api',
    p_pattern        => 'users/',
    p_method         => 'POST',
    p_source_type    => 'plsql/block',
    p_mimes_allowed  => 'application/json',
    p_comments       => 'Create a new user',
    p_source         => 
'BEGIN
    INSERT INTO USERS (
        FIRST_NAME, LAST_NAME, BIRTH_DATE, USERNAME, EMAIL, PASSWORD, ENABLED
    ) VALUES (
        :first_name, :last_name, TO_DATE(:birth_date, ''YYYY-MM-DD''), :username, :email, :password, NVL(:enabled, 1)
    ) RETURNING ID INTO :id;

    -- Standard ORDS response for a successful insert
    :status_code := 201; 
END;'
  );

  -- Define parameters for POST USERS (Important: :id out and :status_code out)
  ORDS.DEFINE_PARAMETER(
      p_module_name        => 'harmony_api',
      p_pattern            => 'users/',
      p_method             => 'POST',
      p_name               => 'id',
      p_bind_variable_name => 'id',
      p_source_type        => 'RESPONSE',
      p_param_type         => 'INT',
      p_access_method      => 'OUT',
      p_comments           => 'Returns generated ID'
  );
  ORDS.DEFINE_PARAMETER(
      p_module_name        => 'harmony_api',
      p_pattern            => 'users/',
      p_method             => 'POST',
      p_name               => 'X-APEX-STATUS-CODE',
      p_bind_variable_name => 'status_code',
      p_source_type        => 'HEADER',
      p_param_type         => 'INT',
      p_access_method      => 'OUT',
      p_comments           => 'Returns 201 Created'
  );

  -- 3. Define Template for a Single User by ID
  -- URL: .../ords/your_workspace/harmony/users/:id
  ORDS.DEFINE_TEMPLATE(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/:id'
  );

  -- Handler: GET a specific user
  ORDS.DEFINE_HANDLER(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/:id',
      p_method         => 'GET',
      p_source_type    => 'json/item',
      p_items_per_page => 0,
      p_mimes_allowed  => '',
      p_comments       => 'Get user by ID',
      p_source         => 'SELECT * FROM USERS WHERE ID = :id AND DELETED = 0'
  );

  -- Handler: PUT (Update) a specific user
  ORDS.DEFINE_HANDLER(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/:id',
      p_method         => 'PUT',
      p_source_type    => 'plsql/block',
      p_items_per_page => 0,
      p_mimes_allowed  => 'application/json',
      p_comments       => 'Update specific user',
      p_source         => 
'BEGIN
    UPDATE USERS SET 
        FIRST_NAME = NVL(:first_name, FIRST_NAME),
        LAST_NAME = NVL(:last_name, LAST_NAME),
        BIRTH_DATE = NVL(TO_DATE(:birth_date, ''YYYY-MM-DD''), BIRTH_DATE),
        USERNAME = NVL(:username, USERNAME),
        EMAIL = NVL(:email, EMAIL),
        PASSWORD = NVL(:password, PASSWORD),
        ENABLED = NVL(:enabled, ENABLED)
    WHERE ID = :id AND DELETED = 0;
    
    IF SQL%ROWCOUNT = 0 THEN
        :status_code := 404; -- Not Found
    ELSE
        :status_code := 200; -- OK
    END IF;
END;'
  );
  ORDS.DEFINE_PARAMETER(
      p_module_name        => 'harmony_api',
      p_pattern            => 'users/:id',
      p_method             => 'PUT',
      p_name               => 'X-APEX-STATUS-CODE',
      p_bind_variable_name => 'status_code',
      p_source_type        => 'HEADER',
      p_param_type         => 'INT',
      p_access_method      => 'OUT',
      p_comments           => 'Returns HTTP status code'
  );

  -- Handler: DELETE a specific user (Logical Delete)
  ORDS.DEFINE_HANDLER(
      p_module_name    => 'harmony_api',
      p_pattern        => 'users/:id',
      p_method         => 'DELETE',
      p_source_type    => 'plsql/block',
      p_items_per_page => 0,
      p_mimes_allowed  => '',
      p_comments       => 'Logical delete user by ID',
      p_source         => 
'BEGIN
    UPDATE USERS SET DELETED = 1, DELETED_AT = CURRENT_TIMESTAMP WHERE ID = :id;
    
    IF SQL%ROWCOUNT = 0 THEN
        :status_code := 404;
    ELSE
        :status_code := 204; -- No Content (standard for successful DELETE)
    END IF;
END;'
  );
  ORDS.DEFINE_PARAMETER(
      p_module_name        => 'harmony_api',
      p_pattern            => 'users/:id',
      p_method             => 'DELETE',
      p_name               => 'X-APEX-STATUS-CODE',
      p_bind_variable_name => 'status_code',
      p_source_type        => 'HEADER',
      p_param_type         => 'INT',
      p_access_method      => 'OUT',
      p_comments           => 'Returns HTTP status code'
  );


  -- ==========================================
  -- PROFILES ENDPOINTS
  -- ==========================================

  -- Template for PROFILES
  ORDS.DEFINE_TEMPLATE(
      p_module_name    => 'harmony_api',
      p_pattern        => 'profiles/'
  );

  -- Handler GET all profiles
  ORDS.DEFINE_HANDLER(
      p_module_name    => 'harmony_api',
      p_pattern        => 'profiles/',
      p_method         => 'GET',
      p_source_type    => 'json/collection',
      p_source         => 'SELECT * FROM PROFILES WHERE DELETED = 0'
  );

  -- Handler: POST a new profile
  ORDS.DEFINE_HANDLER(
    p_module_name    => 'harmony_api',
    p_pattern        => 'profiles/',
    p_method         => 'POST',
    p_source_type    => 'plsql/block',
    p_mimes_allowed  => 'application/json',
    p_source         => 
'BEGIN
    INSERT INTO PROFILES (
        BIO, AVATAR_URL, USER_ID
    ) VALUES (
        :bio, :avatar_url, :user_id
    ) RETURNING ID INTO :id;
    :status_code := 201; 
END;'
  );
  ORDS.DEFINE_PARAMETER(p_module_name => 'harmony_api', p_pattern => 'profiles/', p_method => 'POST', p_name => 'id', p_bind_variable_name => 'id', p_source_type => 'RESPONSE', p_param_type => 'INT', p_access_method => 'OUT');
  ORDS.DEFINE_PARAMETER(p_module_name => 'harmony_api', p_pattern => 'profiles/', p_method => 'POST', p_name => 'X-APEX-STATUS-CODE', p_bind_variable_name => 'status_code', p_source_type => 'HEADER', p_param_type => 'INT', p_access_method => 'OUT');

  COMMIT;
END;
/
