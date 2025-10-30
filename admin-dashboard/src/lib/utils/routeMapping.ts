/**
 * Route mapping utility to translate frontend routes to backend route names.
 * 
 * The backend returns route names like `/services` and `/instances` in `allowedUiRoutes`,
 * but the frontend uses more descriptive routes like `/application-services` and `/service-instances`.
 * This utility maps frontend routes to their corresponding backend route names for permission checks.
 */

/**
 * Maps a frontend route to its corresponding backend route name.
 * 
 * @param frontendRoute - The frontend route path (e.g., `/application-services`)
 * @returns The backend route name used in permissions (e.g., `/services`)
 */
export function mapFrontendToBackendRoute(frontendRoute: string): string {
  const routeMap: Record<string, string> = {
    "/application-services": "/services",
    "/service-instances": "/instances",
  };

  return routeMap[frontendRoute] || frontendRoute;
}

/**
 * Maps a backend route to its corresponding frontend route name.
 * 
 * @param backendRoute - The backend route name (e.g., `/services`)
 * @returns The frontend route path (e.g., `/application-services`)
 */
export function mapBackendToFrontendRoute(backendRoute: string): string {
  const routeMap: Record<string, string> = {
    "/services": "/application-services",
    "/instances": "/service-instances",
  };

  return routeMap[backendRoute] || backendRoute;
}

