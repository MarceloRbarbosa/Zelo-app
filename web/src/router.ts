// Hash routes (#/turmas/3) keep the app a single static page that works behind any host.
export interface Route {
  path: string;
  params: Record<string, string>;
}

export function currentRoute(): Route {
  const path = location.hash.replace(/^#/, '') || '/';
  return { path, params: {} };
}

export function match(pattern: string, path: string): Record<string, string> | null {
  const patternParts = pattern.split('/').filter(Boolean);
  const pathParts = path.split('?')[0].split('/').filter(Boolean);
  if (patternParts.length !== pathParts.length) {
    return null;
  }
  const params: Record<string, string> = {};
  for (let index = 0; index < patternParts.length; index += 1) {
    const part = patternParts[index];
    if (part.startsWith(':')) {
      params[part.slice(1)] = decodeURIComponent(pathParts[index]);
    } else if (part !== pathParts[index]) {
      return null;
    }
  }
  return params;
}

export function navigate(path: string): void {
  location.hash = path;
}
