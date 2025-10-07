import { Suspense, lazy } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import MainLayout from '@layout/MainLayout'
import Loading from '@components/common/Loading'
import ErrorBoundary from '@components/ErrorBoundary'
import NotFoundPage from '../pages/NotFoundPage'

const DashboardPage = lazy(() => import('@features/dashboard/pages/DashboardPage'))
const ServiceListPage = lazy(() => import('@features/services/pages/ServiceListPage'))
const ServiceDetailPage = lazy(() => import('@features/services/pages/ServiceDetailPage'))
const ConfigListPage = lazy(() => import('@features/configs/pages/ConfigListPage'))
const ConfigDetailPage = lazy(() => import('@features/configs/pages/ConfigDetailPage'))

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    errorElement: <ErrorBoundary><div>Error occurred</div></ErrorBoundary>,
    children: [
      { 
        index: true, 
        element: (
          <Suspense fallback={<Loading />}> 
            <DashboardPage />
          </Suspense>
        )
      },
      {
        path: 'services',
        children: [
          {
            index: true,
            element: (
              <Suspense fallback={<Loading />}> 
                <ServiceListPage />
              </Suspense>
            ),
          },
          {
            path: ':serviceName',
            element: (
              <Suspense fallback={<Loading />}> 
                <ServiceDetailPage />
              </Suspense>
            ),
          },
        ],
      },
      {
        path: 'configs',
        children: [
          {
            index: true,
            element: (
              <Suspense fallback={<Loading />}> 
                <ConfigListPage />
              </Suspense>
            ),
          },
          {
            path: ':application/:profile',
            element: (
              <Suspense fallback={<Loading />}> 
                <ConfigDetailPage />
              </Suspense>
            ),
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />
      },
    ],
  },
])

export default router


