import { Component } from '@angular/core';
import { Hero } from '../hero/hero';
import { FeaturedEvents } from '../featured-events/featured-events';

@Component({
  selector: 'app-home-page',
  imports: [Hero, FeaturedEvents],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {}