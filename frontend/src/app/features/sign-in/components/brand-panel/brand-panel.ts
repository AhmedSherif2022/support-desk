import { Component } from '@angular/core';

@Component({
  selector: 'app-brand-panel',
  templateUrl: './brand-panel.html',
})
export class BrandPanel {
  points = [
    { 
      icon: 'pi-shield', 
      text: 'Nothing about a customer opens until the caller is verified.' 
    },
    { 
      icon: 'pi-ticket', 
      text: 'Logs, tickets, guides and SRE state in one place.' 
    },
    { 
      icon: 'pi-book', 
      text: 'Every guide is approved by a second Guide Admin before it goes live.' 
    },
  ];
}