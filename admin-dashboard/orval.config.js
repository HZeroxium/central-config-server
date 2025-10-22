export default {
    'config-control-api': {
      input: {
        target: '../config-control-service/spec/openapi.json',
        filter: {
          tags: ['Application Services', 'Service Shares', 'Approval Requests', 'IAM Users', 'IAM Teams', 'User Management', 'Drift Events', 'Service Instances', 'Service Registry', 'Config Server'],
        },
      },
      output: {
        target: './src/lib/api/generated/',
        client: 'react-query',
        mode: 'tags-split',
        schemas: './src/lib/api/models',
        httpClient: 'axios',
        override: {
          mutator: {
            path: './src/lib/api/mutator.ts',
            name: 'customInstance',
          },
          query: {
            useQuery: true,
            useInfinite: true,
            useInfiniteQueryParam: 'page',
            useInfiniteQueryOptions: {
              getNextPageParam: (lastPage, allPages) => {
                return lastPage.hasNext ? allPages.length : undefined;
              }
            }
          },
        },
      },
    },
  };