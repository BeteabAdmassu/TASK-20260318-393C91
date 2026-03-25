import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SearchResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class SearchService {
  constructor(private readonly http: HttpClient) {}

  search(query: string): Observable<SearchResponse> {
    return this.http.get<SearchResponse>('/api/passenger/search', {
      params: { q: query }
    });
  }
}
