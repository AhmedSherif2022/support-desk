import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Stand-in for pages that are not built yet. The route supplies `title` through its `data`. */
@Component({
  selector: 'app-placeholder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="ds-h1">{{ title() }}</h1>
    <p class="ds-body">This page is not built yet.</p>
  `,
})
export class Placeholder {
  readonly title = input('');
}
