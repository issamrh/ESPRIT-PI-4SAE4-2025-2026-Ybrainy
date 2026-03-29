import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { CategoryFormComponent } from './category-form/category-form.component';
import { CategoryListComponent } from './category-list/category-list.component';
import { FinanceComponent } from './finance/finance.component';
import { PackFormComponent } from './pack-form/pack-form.component';
import { PackListComponent } from './pack-list/pack-list.component';
import { PacksOrderComponent } from './packs-order/packs-order.component';

@NgModule({
  declarations: [
    CategoryFormComponent,
    CategoryListComponent,
    FinanceComponent,
    PackFormComponent,
    PackListComponent,
    PacksOrderComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
  ],
})
export class MergedBackofficeModule {}
