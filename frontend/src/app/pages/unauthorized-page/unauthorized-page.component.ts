import {Component} from '@angular/core';
import {Router} from "@angular/router";
import {AuthService} from "../../core/auth.service";

@Component({
  selector: 'app-unauthorized-page',
  standalone: true,
  imports: [],
  templateUrl: './unauthorized-page.component.html',
  styleUrl: './unauthorized-page.component.scss'
})
export class UnauthorizedPageComponent {

  constructor(private router: Router, private authService: AuthService) {
  }

  public enter(apiKey: string): void {
    this.authService.setKey(apiKey);
    this.router.navigate(['/']);
  }

}
