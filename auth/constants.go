
package auth

const (
    RedirectUri   = "https://<your-auth-domain>/auth/callback"
    AuthEp        = "https://auth.tesla.com/oauth2/v3/authorize"
    Scope         = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
    TokenEp       = "https://auth.tesla.com/oauth2/v3/token"
    Audience      = "https://fleet-api.prd.na.vn.cloud.tesla.com"
    AuthCacheFile = ".tesla_auth_cache.json"
)

