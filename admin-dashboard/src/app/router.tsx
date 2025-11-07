import { Suspense, lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import MainLayout from "@layout/MainLayout";
import Loading from "@components/common/Loading";
import ErrorBoundary from "@components/ErrorBoundary";
import NotFoundPage from "../pages/NotFoundPage";
import ProtectedRoute from "@components/auth/ProtectedRoute";

// Lazy load all pages
const DashboardPage = lazy(() =>
  import("@features/dashboard/pages/DashboardPage").then((module) => ({
    default: module.default,
  }))
);
const ConfigListPage = lazy(
  () => import("@features/configs/pages/ConfigListPage")
);
const ConfigDetailPage = lazy(
  () => import("@features/configs/pages/ConfigDetailPage")
);
const ApplicationServiceListPage = lazy(
  () =>
    import("@features/application-services/pages/ApplicationServiceListPage")
);
const ApplicationServiceDetailPage = lazy(
  () =>
    import("@features/application-services/pages/ApplicationServiceDetailPage")
);
const ServiceInstanceListPage = lazy(
  () => import("@features/service-instances/pages/ServiceInstanceListPage")
);
const ServiceInstanceDetailPage = lazy(
  () => import("@features/service-instances/pages/ServiceInstanceDetailPage")
);
const ApprovalListPage = lazy(
  () => import("@features/approvals/pages/ApprovalListPage")
);
const ApprovalDetailPage = lazy(
  () => import("@features/approvals/pages/ApprovalDetailPage")
);
const ApprovalDecisionListPage = lazy(
  () => import("@features/approvals/pages/ApprovalDecisionListPage")
);
const ApprovalDecisionDetailPage = lazy(
  () => import("@features/approvals/pages/ApprovalDecisionDetailPage")
);
const DriftEventListPage = lazy(
  () => import("@features/drift-events/pages/DriftEventListPage")
);
const DriftEventDetailPage = lazy(
  () => import("@features/drift-events/pages/DriftEventDetailPage")
);
const ServiceShareListPage = lazy(
  () => import("@features/service-shares/pages/ServiceShareListPage")
);
const ServiceShareDetailPage = lazy(
  () => import("@features/service-shares/pages/ServiceShareDetailPage")
);
const IamUserListPage = lazy(
  () => import("@features/iam/pages/IamUserListPage")
);
const IamTeamListPage = lazy(
  () => import("@features/iam/pages/IamTeamListPage")
);
const ServiceRegistryListPage = lazy(
  () => import("@features/service-registry/pages/ServiceRegistryListPage")
);
const ServiceRegistryDetailPage = lazy(
  () => import("@features/service-registry/pages/ServiceRegistryDetailPage")
);
const ProfilePage = lazy(() => import("@features/auth/pages/ProfilePage"));
const LoginCallbackPage = lazy(
  () => import("@features/auth/pages/LoginCallbackPage")
);
const UnauthorizedPage = lazy(() => import("../pages/UnauthorizedPage"));
const KVStoreListPage = lazy(
  () => import("@features/key-value-store/pages/KVStoreListPage")
);
const KVStorePage = lazy(
  () => import("@features/key-value-store/pages/KVStorePage")
);

export const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    errorElement: (
      <ErrorBoundary>
        <div>Error occurred</div>
      </ErrorBoundary>
    ),
    children: [
      {
        index: true,
        element: (
          <ProtectedRoute requiredRoute="/dashboard">
            <Suspense fallback={<Loading />}>
              <DashboardPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: "dashboard",
        element: (
          <ProtectedRoute requiredRoute="/dashboard">
            <Suspense fallback={<Loading />}>
              <DashboardPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: "services",
        element: <Navigate to="/application-services" replace />,
      },
      {
        path: "application-services",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute requiredRoute="/application-services">
                <Suspense fallback={<Loading />}>
                  <ApplicationServiceListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":id",
            element: (
              <ProtectedRoute requiredRoute="/application-services">
                <Suspense fallback={<Loading />}>
                  <ApplicationServiceDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "configs",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ConfigListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":application/:profile",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ConfigDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "kv",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <KVStoreListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":serviceId",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <KVStorePage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":serviceId/*",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <KVStorePage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "instances",
        element: <Navigate to="/service-instances" replace />,
      },
      {
        path: "service-instances",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute requiredRoute="/service-instances">
                <Suspense fallback={<Loading />}>
                  <ServiceInstanceListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":serviceName/:instanceId",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ServiceInstanceDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "approvals",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ApprovalListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":id",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ApprovalDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "approval-decisions",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ApprovalDecisionListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":id",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ApprovalDecisionDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "drift-events",
        element: (
          <ProtectedRoute requiredRoute="/drift-events">
            <Suspense fallback={<Loading />}>
              <DriftEventListPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: "drift-events/:id",
        element: (
          <ProtectedRoute>
            <Suspense fallback={<Loading />}>
              <DriftEventDetailPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: "service-shares",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ServiceShareListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":id",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ServiceShareDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "registry",
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ServiceRegistryListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: ":serviceName",
            element: (
              <ProtectedRoute>
                <Suspense fallback={<Loading />}>
                  <ServiceRegistryDetailPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "iam",
        children: [
          {
            path: "users",
            element: (
              <ProtectedRoute
                requiredRoute="/iam/users"
                requiredRoles={["SYS_ADMIN"]}
              >
                <Suspense fallback={<Loading />}>
                  <IamUserListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
          {
            path: "teams",
            element: (
              <ProtectedRoute
                requiredRoute="/iam/teams"
                requiredRoles={["SYS_ADMIN"]}
              >
                <Suspense fallback={<Loading />}>
                  <IamTeamListPage />
                </Suspense>
              </ProtectedRoute>
            ),
          },
        ],
      },
      {
        path: "profile",
        element: (
          <ProtectedRoute>
            <Suspense fallback={<Loading />}>
              <ProfilePage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: "login-callback",
        element: (
          <Suspense fallback={<Loading />}>
            <LoginCallbackPage />
          </Suspense>
        ),
      },
      {
        path: "unauthorized",
        element: (
          <Suspense fallback={<Loading />}>
            <UnauthorizedPage />
          </Suspense>
        ),
      },
      {
        path: "*",
        element: <NotFoundPage />,
      },
    ],
  },
]);

export default router;
