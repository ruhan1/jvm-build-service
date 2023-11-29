import * as React from 'react';
import { Route, RouteComponentProps, Switch, useLocation } from 'react-router-dom';
import { Dashboard } from '@app/Dashboard/Dashboard';
import { NotFound } from '@app/NotFound/NotFound';
import { useDocumentTitle } from '@app/utils/useDocumentTitle';
import {GithubBuildList} from "@app/GithubBuildList/GithubBuildList";
import {BuildQueueList} from "@app/BuildQueueList/BuildQueueList";
import {BuildView} from "@app/BuildView/BuildView";
import {DeploymentList} from "@app/DeploymentList/DeploymentList";
import {AddArtifact} from "@app/AddArtifact/AddArtifact";
import {ArtifactList} from "@app/ArtifactList/ArtifactList";
import {BuildList} from "@app/BuildList/BuildList";
import {RunningBuildList} from "@app/RunningBuildList/RunningBuildList";

let routeFocusTimer: number;
export interface IAppRoute {
  label?: string; // Excluding the label will exclude the route from the nav sidebar in AppLayout
  /* eslint-disable @typescript-eslint/no-explicit-any */
  component: React.ComponentType<RouteComponentProps<any>> | React.ComponentType<any>;
  /* eslint-enable @typescript-eslint/no-explicit-any */
  exact?: boolean;
  path: string;
  title: string;
  routes?: undefined;
}

export interface IAppRouteGroup {
  label: string;
  routes: IAppRoute[];
}

export type AppRouteConfig = IAppRoute | IAppRouteGroup;

const routes: AppRouteConfig[] = [
  {
    component: Dashboard,
    exact: true,
    label: 'Home',
    path: '/',
    title: 'JVM Build Service',
  },
  {
    label: 'Builds',
    routes: [
      {
        component: BuildList,
        exact: true,
        label: 'All Builds',
        path: '/builds/all',
        title: 'JVM Build Service | Build List',
      },
      {
        component: BuildView,
        exact: true,
        path: '/builds/build/:id',
        title: 'JVM Build Service | Build',
      },
      {
        component: RunningBuildList,
        exact: true,
        label: 'Running Builds',
        path: '/builds/running',
        title: 'JVM Build Service | Running Build List',
      },
      {
        component: BuildQueueList,
        exact: true,
        label: 'Build Queue',
        path: '/builds/queue',
        title: 'JVM Build Service | Build Queue',
      },
    ],
  },
  {
    label: 'Artifacts',
    routes: [
      {
        component: ArtifactList,
        exact: true,
        label: 'All Artifacts',
        path: '/artifacts/all',
        title: 'JVM Build Service | Artifact List',
      },
      {
        component: AddArtifact,
        exact: true,
        label: 'Add Artifact',
        path: '/artifacts/create',
        title: 'JVM Build Service | Add Artifact',
      },
    ],
  },
  {
    label: 'Deployments',
    routes: [
      {
        component: DeploymentList,
        exact: true,
        label: 'All Deployments',
        path: '/deployments/all',
        title: 'JVM Build Service | Deployment List',
      },
    ],
  },
  {
    label: 'CI',
    routes: [
      {
        component: GithubBuildList,
        exact: true,
        label: 'Github Actions',
        path: '/builds/github/all',
        title: 'JVM Build Service | Github Actions Builds',
      },
    ],
  },
];

// a custom hook for sending focus to the primary content container
// after a view has loaded so that subsequent press of tab key
// sends focus directly to relevant content
// may not be necessary if https://github.com/ReactTraining/react-router/issues/5210 is resolved
const useA11yRouteChange = () => {
  const { pathname } = useLocation();
  React.useEffect(() => {
    routeFocusTimer = window.setTimeout(() => {
      const mainContainer = document.getElementById('primary-app-container');
      if (mainContainer) {
        mainContainer.focus();
      }
    }, 50);
    return () => {
      window.clearTimeout(routeFocusTimer);
    };
  }, [pathname]);
};

const RouteWithTitleUpdates = ({ component: Component, title, ...rest }: IAppRoute) => {
  useA11yRouteChange();
  useDocumentTitle(title);

  function routeWithTitle(routeProps: RouteComponentProps) {
    return <Component {...rest} {...routeProps} />;
  }

  return <Route render={routeWithTitle} {...rest} />;
};

const PageNotFound = ({ title }: { title: string }) => {
  useDocumentTitle(title);
  return <Route component={NotFound} />;
};

const flattenedRoutes: IAppRoute[] = routes.reduce(
  (flattened, route) => [...flattened, ...(route.routes ? route.routes : [route])],
  [] as IAppRoute[]
);

const AppRoutes = (): React.ReactElement => (
  <Switch>
    {flattenedRoutes.map(({ path, exact, component, title }, idx) => (
      <RouteWithTitleUpdates path={path} exact={exact} component={component} key={idx} title={title} />
    ))}
    <PageNotFound title="404 Page Not Found" />
  </Switch>
);

export { AppRoutes, routes };
